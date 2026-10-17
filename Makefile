CC      = gcc
# Cada subdiretório de src/ é um diretório de cabeçalhos: os #include usam só o nome do arquivo
CFLAGS  = -Wall -Wextra -std=gnu11 -g -Isrc/nucleo -Isrc/atendimento -Isrc/comum
# Em versões da glibc anteriores à 2.34, sem_open/sem_wait ficam na libpthread.
# Nenhuma função pthread_* é usada; a opção serve apenas para ligar os semáforos.
LDLIBS  = -pthread

PROGRAMAS = atendimento analista cliente

# Fontes compartilhados pelo Atendimento e pelos testes (todos, exceto o main de atendimento.c)
FONTES     = src/nucleo/nucleo.c $(filter-out src/atendimento/atendimento.c,$(wildcard src/atendimento/*.c))
CABECALHOS = $(wildcard src/*/*.h)
TESTES     = $(patsubst tests/%.c,build/tests/%,$(wildcard tests/teste_*.c))

.PHONY: all test clean

all: $(PROGRAMAS)

# Os executáveis ficam na raiz porque o Atendimento cria os clientes com ./cliente
# e todos compartilham o demanda.txt do diretório corrente.
atendimento: src/atendimento/atendimento.c $(FONTES) $(CABECALHOS)
	$(CC) $(CFLAGS) src/atendimento/atendimento.c $(FONTES) -o $@ $(LDLIBS)

%: src/processos/%.c src/comum/comum.h
	$(CC) $(CFLAGS) $< -o $@ $(LDLIBS)

build/tests/%: tests/%.c tests/teste.h $(FONTES) $(CABECALHOS)
	@mkdir -p build/tests
	$(CC) $(CFLAGS) $< $(FONTES) -o $@ $(LDLIBS)

# Os testes de integração executam os programas compilados
test: $(PROGRAMAS) $(TESTES)
	@echo "Executando testes:"
	@for t in $(TESTES); do ./$$t || exit 1; done

clean:
	rm -rf build $(PROGRAMAS) demanda.txt lng.txt lng.tmp pid_analista.txt
