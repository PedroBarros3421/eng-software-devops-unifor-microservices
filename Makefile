MAVEN_REPO ?= /tmp/m2
MVN_TEST = mvn test -Dmaven.repo.local=$(MAVEN_REPO) -Dsurefire.printSummary=true
PYTHON ?= python3
VENV ?= .venv
PERF_PYTHON = $(VENV)/bin/python
PIP = $(VENV)/bin/pip

.PHONY: test test-contratos test-compras test-vendas test-all perf perf-setup

test: test-all

test-all:
	$(MAKE) test-contratos
	$(MAKE) test-compras
	$(MAKE) test-vendas

test-contratos:
	cd contratos-service && $(MVN_TEST)

test-compras:
	cd compras-service && $(MVN_TEST)

test-vendas:
	cd vendas-service && $(MVN_TEST)

perf: $(PERF_PYTHON)
	mkdir -p tests/performance/results
	$(PERF_PYTHON) tests/performance/perf_observability.py

perf-setup:
	$(PYTHON) -m venv $(VENV)
	$(PIP) install -r tests/performance/requirements.txt

$(PERF_PYTHON): tests/performance/requirements.txt
	$(PYTHON) -m venv $(VENV)
	$(PIP) install -r tests/performance/requirements.txt
