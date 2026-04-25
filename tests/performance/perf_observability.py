#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import math
import os
import random
import threading
import time
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from datetime import date, timedelta
from pathlib import Path
from typing import Any

import requests
from requests import Response, Session
from requests.exceptions import RequestException
from rich.console import Console
from rich.table import Table


console = Console()


@dataclass
class ResponseResult:
    operation: str
    method: str
    path: str
    status: int
    duration_ms: float
    ok: bool
    business_ok: bool
    error: str | None = None


DEFAULT_OUTPUT_DIR = Path("tests/performance/results")


def env_float(name: str, default: float) -> float:
    return float(os.getenv(name, default))


def env_int(name: str, default: int) -> int:
    return int(os.getenv(name, default))


def request_json(
    session: Session,
    method: str,
    url: str,
    payload: dict[str, Any] | None = None,
    timeout: float = 10.0,
) -> tuple[int, Any, float]:
    started = time.perf_counter()
    try:
        response = session.request(
            method=method,
            url=url,
            json=payload,
            headers={"Accept": "application/json"},
            timeout=timeout,
        )
        duration_ms = (time.perf_counter() - started) * 1000
        try:
            body = response.json() if response.text else None
        except json.JSONDecodeError:
            body = response.text
        return response.status_code, body, duration_ms
    except RequestException as exc:
        duration_ms = (time.perf_counter() - started) * 1000
        return 0, str(exc), duration_ms


def percentile(values: list[float], p: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    index = (len(ordered) - 1) * p
    lower = math.floor(index)
    upper = math.ceil(index)
    if lower == upper:
        return ordered[int(index)]
    weight = index - lower
    return ordered[lower] * (1 - weight) + ordered[upper] * weight


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# Performance Summary",
        "",
        f"- Base URL: `{summary['base_url']}`",
        f"- Duration: `{summary['duration_seconds']}s`",
        f"- Workers: `{summary['workers']}`",
        f"- Pesos de carga: `contratos={summary['weights']['list_contracts']}, estoque={summary['weights']['list_inventory']}, vendas={summary['weights']['create_sale']}`",
        f"- Total requests: `{summary['total_requests']}`",
        f"- Throughput médio: `{summary['throughput_rps']:.2f} req/s`",
        "",
        "## SLI Medidos",
        "",
        f"- Disponibilidade técnica: `{summary['sli']['availability'] * 100:.2f}%`",
        f"- Sucesso de negócio: `{summary['sli']['business_success'] * 100:.2f}%`",
        f"- Latência P50 global: `{summary['global_percentiles']['p50_ms']:.2f} ms`",
        f"- Latência P95 global: `{summary['global_percentiles']['p95_ms']:.2f} ms`",
        f"- Latência P99 global: `{summary['global_percentiles']['p99_ms']:.2f} ms`",
        "",
        "## Metas",
        "",
        f"- SLO disponibilidade >= {summary['targets']['slo']['availability'] * 100:.2f}% -> {summary['evaluations']['slo']['availability']}",
        f"- SLO P95 <= {summary['targets']['slo']['p95_ms']:.2f} ms -> {summary['evaluations']['slo']['p95']}",
        f"- SLO P99 <= {summary['targets']['slo']['p99_ms']:.2f} ms -> {summary['evaluations']['slo']['p99']}",
        f"- SLA disponibilidade >= {summary['targets']['sla']['availability'] * 100:.2f}% -> {summary['evaluations']['sla']['availability']}",
        f"- SLA P95 <= {summary['targets']['sla']['p95_ms']:.2f} ms -> {summary['evaluations']['sla']['p95']}",
        f"- SLA P99 <= {summary['targets']['sla']['p99_ms']:.2f} ms -> {summary['evaluations']['sla']['p99']}",
        "",
        "## Percentis por Operação",
        "",
        "| Operação | Requests | Erros | P50 | P95 | P99 |",
        "|---|---:|---:|---:|---:|---:|",
    ]

    for operation, values in summary["operations"].items():
        lines.append(
            f"| {operation} | {values['requests']} | {values['errors']} | "
            f"{values['p50_ms']:.2f} ms | {values['p95_ms']:.2f} ms | {values['p99_ms']:.2f} ms |"
        )

    lines.extend(
        [
            "",
            "## Status por Operação",
            "",
        ]
    )

    for operation, values in summary["operations"].items():
        status_parts = [f"`{status}`: {count}" for status, count in values["statuses"].items()]
        lines.append(f"- {operation}: {', '.join(status_parts)}")

    lines.extend(
        [
            "",
            "## Interpretação",
            "",
            "- P50, P95 e P99 representam os percentis de latência medidos durante a execução.",
            "- SLI são os indicadores efetivamente observados no teste.",
            "- SLO e SLA são metas de referência configuradas no script para comparação.",
        ]
    )

    return "\n".join(lines)


def render_console_summary(summary: dict[str, Any]) -> None:
    console.print("[bold]Performance Summary[/bold]")
    console.print(
        f"Base URL: {summary['base_url']} | Duration: {summary['duration_seconds']}s | "
        f"Workers: {summary['workers']} | Throughput: {summary['throughput_rps']:.2f} req/s"
    )
    console.print(
        "SLI availability: "
        f"{summary['sli']['availability'] * 100:.2f}% | "
        "Business success: "
        f"{summary['sli']['business_success'] * 100:.2f}%"
    )

    table = Table(title="Percentis por Operação")
    table.add_column("Operação")
    table.add_column("Requests", justify="right")
    table.add_column("Erros", justify="right")
    table.add_column("P50", justify="right")
    table.add_column("P95", justify="right")
    table.add_column("P99", justify="right")

    for operation, values in summary["operations"].items():
        table.add_row(
            operation,
            str(values["requests"]),
            str(values["errors"]),
            f"{values['p50_ms']:.2f} ms",
            f"{values['p95_ms']:.2f} ms",
            f"{values['p99_ms']:.2f} ms",
        )
    console.print(table)

    status_table = Table(title="Status por Operação")
    status_table.add_column("Operação")
    status_table.add_column("Status")
    for operation, values in summary["operations"].items():
        parts = [f"{status}: {count}" for status, count in values["statuses"].items()]
        status_table.add_row(operation, ", ".join(parts))
    console.print(status_table)


class Runner:
    def __init__(self, args: argparse.Namespace) -> None:
        self.base_url = args.base_url.rstrip("/")
        self.duration_seconds = args.duration
        self.workers = args.workers
        self.timeout = args.timeout
        self.output_dir = Path(args.output_dir)
        self.stop_at = time.perf_counter() + self.duration_seconds
        self.lock = threading.Lock()
        self.results: list[ResponseResult] = []
        self.contract_id: int | None = None
        self.insumo_id: int | None = None
        self.thread_local = threading.local()

        self.slo = {
            "availability": args.slo_availability,
            "p95_ms": args.slo_p95_ms,
            "p99_ms": args.slo_p99_ms,
        }
        self.sla = {
            "availability": args.sla_availability,
            "p95_ms": args.sla_p95_ms,
            "p99_ms": args.sla_p99_ms,
        }
        self.operation_pool = (
            ["list_contracts"] * max(1, args.contracts_weight)
            + ["list_inventory"] * max(1, args.inventory_weight)
            + ["create_sale"] * max(1, args.sales_weight)
        )

    def session(self) -> Session:
        if not hasattr(self.thread_local, "session"):
            self.thread_local.session = requests.Session()
        return self.thread_local.session

    def add_result(self, result: ResponseResult) -> None:
        with self.lock:
            self.results.append(result)

    def setup(self) -> None:
        suffix = int(time.time() * 1000)
        today = date.today()
        payload_contrato = {
            "numero": f"CTR-PERF-{suffix}",
            "nomeContratante": "Cliente Performance",
            "valorTotal": 200000,
            "dataInicio": today.isoformat(),
            "dataFim": (today + timedelta(days=30)).isoformat(),
            "status": "ATIVO",
            "termos": "Contrato criado para teste de performance",
        }
        status, body, _ = request_json(
            self.session(),
            "POST",
            f"{self.base_url}/api/contratos",
            payload_contrato,
            timeout=self.timeout,
        )
        if status != 201 or not isinstance(body, dict) or "id" not in body:
            raise RuntimeError(f"Falha no setup de contrato: status={status}, body={body}")
        self.contract_id = int(body["id"])

        payload_insumo = {
            "nome": f"Insumo Performance {suffix}",
            "descricao": "Insumo criado para teste de performance",
            "unidadeMedida": "UN",
            "precoUnitario": 10.0,
            "quantidadeEstoque": 100000,
        }
        status, body, _ = request_json(
            self.session(),
            "POST",
            f"{self.base_url}/api/compras/insumos",
            payload_insumo,
            timeout=self.timeout,
        )
        if status != 201 or not isinstance(body, dict) or "id" not in body:
            raise RuntimeError(f"Falha no setup de insumo: status={status}, body={body}")
        self.insumo_id = int(body["id"])

    def run_operation(self, operation: str) -> ResponseResult:
        started = time.perf_counter()
        try:
            if operation == "list_contracts":
                status, _, duration_ms = request_json(
                    self.session(),
                    "GET",
                    f"{self.base_url}/api/contratos",
                    timeout=self.timeout,
                )
                return ResponseResult(operation, "GET", "/api/contratos", status, duration_ms, status == 200, status == 200)

            if operation == "list_inventory":
                status, _, duration_ms = request_json(
                    self.session(),
                    "GET",
                    f"{self.base_url}/api/compras/insumos",
                    timeout=self.timeout,
                )
                return ResponseResult(operation, "GET", "/api/compras/insumos", status, duration_ms, status == 200, status == 200)

            if operation == "create_sale":
                payload = {
                    "nomeCliente": "Cliente Performance",
                    "contratoId": self.contract_id,
                    "itens": [
                        {
                            "insumoId": self.insumo_id,
                            "nomeInsumo": "Insumo Performance",
                            "quantidade": 1,
                            "precoUnitario": 10.0,
                        }
                    ],
                }
                status, body, duration_ms = request_json(
                    self.session(),
                    "POST",
                    f"{self.base_url}/api/vendas/pedidos",
                    payload,
                    timeout=self.timeout,
                )
                business_ok = status == 201 and isinstance(body, dict) and body.get("id")
                return ResponseResult(operation, "POST", "/api/vendas/pedidos", status, duration_ms, status == 201, bool(business_ok))

            raise ValueError(f"Operação desconhecida: {operation}")
        except Exception as exc:  # noqa: BLE001
            duration_ms = (time.perf_counter() - started) * 1000
            return ResponseResult(operation, "ERR", operation, 0, duration_ms, False, False, str(exc))

    def worker(self, worker_id: int) -> None:
        rng = random.Random(worker_id + int(time.time()))
        while time.perf_counter() < self.stop_at:
            operation = rng.choice(self.operation_pool)
            result = self.run_operation(operation)
            self.add_result(result)

    def summarize(self) -> dict[str, Any]:
        total_requests = len(self.results)
        technical_successes = sum(1 for item in self.results if item.ok)
        business_successes = sum(1 for item in self.results if item.business_ok)
        error_count = total_requests - technical_successes
        durations = [item.duration_ms for item in self.results]
        per_operation: dict[str, list[ResponseResult]] = defaultdict(list)
        for item in self.results:
            per_operation[item.operation].append(item)

        operations = {}
        for operation, items in per_operation.items():
            op_durations = [item.duration_ms for item in items]
            op_errors = sum(1 for item in items if not item.ok)
            statuses: dict[str, int] = defaultdict(int)
            for item in items:
                statuses[str(item.status)] += 1
            operations[operation] = {
                "requests": len(items),
                "errors": op_errors,
                "p50_ms": percentile(op_durations, 0.50),
                "p95_ms": percentile(op_durations, 0.95),
                "p99_ms": percentile(op_durations, 0.99),
                "statuses": dict(sorted(statuses.items(), key=lambda pair: pair[0])),
            }

        availability = technical_successes / total_requests if total_requests else 0.0
        business_success = business_successes / total_requests if total_requests else 0.0
        global_p50 = percentile(durations, 0.50)
        global_p95 = percentile(durations, 0.95)
        global_p99 = percentile(durations, 0.99)
        throughput = total_requests / self.duration_seconds if self.duration_seconds else 0.0

        summary = {
            "generated_at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
            "base_url": self.base_url,
            "duration_seconds": self.duration_seconds,
            "workers": self.workers,
            "weights": {
                "list_contracts": self.operation_pool.count("list_contracts"),
                "list_inventory": self.operation_pool.count("list_inventory"),
                "create_sale": self.operation_pool.count("create_sale"),
            },
            "total_requests": total_requests,
            "technical_successes": technical_successes,
            "business_successes": business_successes,
            "errors": error_count,
            "throughput_rps": throughput,
            "sli": {
                "availability": availability,
                "business_success": business_success,
            },
            "global_percentiles": {
                "p50_ms": global_p50,
                "p95_ms": global_p95,
                "p99_ms": global_p99,
            },
            "targets": {
                "slo": self.slo,
                "sla": self.sla,
            },
            "evaluations": {
                "slo": {
                    "availability": "OK" if availability >= self.slo["availability"] else "FAIL",
                    "p95": "OK" if global_p95 <= self.slo["p95_ms"] else "FAIL",
                    "p99": "OK" if global_p99 <= self.slo["p99_ms"] else "FAIL",
                },
                "sla": {
                    "availability": "OK" if availability >= self.sla["availability"] else "FAIL",
                    "p95": "OK" if global_p95 <= self.sla["p95_ms"] else "FAIL",
                    "p99": "OK" if global_p99 <= self.sla["p99_ms"] else "FAIL",
                },
            },
            "operations": operations,
            "failures": [
                {
                    "operation": item.operation,
                    "status": item.status,
                    "duration_ms": item.duration_ms,
                    "error": item.error,
                }
                for item in self.results
                if not item.ok
            ][:30],
        }
        return summary

    def write_outputs(self, summary: dict[str, Any]) -> None:
        self.output_dir.mkdir(parents=True, exist_ok=True)
        (self.output_dir / "summary.json").write_text(
            json.dumps(summary, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )
        (self.output_dir / "summary.md").write_text(
            build_markdown(summary),
            encoding="utf-8",
        )

    def execute(self) -> dict[str, Any]:
        self.setup()
        with ThreadPoolExecutor(max_workers=self.workers) as executor:
            for worker_id in range(self.workers):
                executor.submit(self.worker, worker_id)
        summary = self.summarize()
        self.write_outputs(summary)
        return summary


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Executa carga leve/ média nas rotas do sistema e calcula percentis, SLI, SLO e SLA."
    )
    parser.add_argument("--base-url", default=os.getenv("BASE_URL", "http://localhost:8080"))
    parser.add_argument("--duration", type=int, default=env_int("DURATION_SECONDS", 60))
    parser.add_argument("--workers", type=int, default=env_int("WORKERS", 12))
    parser.add_argument("--timeout", type=float, default=env_float("TIMEOUT_SECONDS", 10.0))
    parser.add_argument("--output-dir", default=os.getenv("OUTPUT_DIR", str(DEFAULT_OUTPUT_DIR)))
    parser.add_argument("--slo-availability", type=float, default=env_float("SLO_AVAILABILITY", 0.99))
    parser.add_argument("--slo-p95-ms", type=float, default=env_float("SLO_P95_MS", 500.0))
    parser.add_argument("--slo-p99-ms", type=float, default=env_float("SLO_P99_MS", 1000.0))
    parser.add_argument("--sla-availability", type=float, default=env_float("SLA_AVAILABILITY", 0.95))
    parser.add_argument("--sla-p95-ms", type=float, default=env_float("SLA_P95_MS", 800.0))
    parser.add_argument("--sla-p99-ms", type=float, default=env_float("SLA_P99_MS", 1500.0))
    parser.add_argument("--contracts-weight", type=int, default=env_int("CONTRACTS_WEIGHT", 3))
    parser.add_argument("--inventory-weight", type=int, default=env_int("INVENTORY_WEIGHT", 3))
    parser.add_argument("--sales-weight", type=int, default=env_int("SALES_WEIGHT", 6))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    runner = Runner(args)
    summary = runner.execute()
    render_console_summary(summary)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
