MAVEN_REPO ?= /tmp/m2
MVN_TEST = mvn test -Dmaven.repo.local=$(MAVEN_REPO) -Dsurefire.printSummary=true

.PHONY: test test-contratos test-compras test-vendas test-all

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
