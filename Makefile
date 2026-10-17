CC      = gcc
CFLAGS  = -Wall -Wextra -std=gnu11 -g
# Em versões da glibc anteriores à 2.34, sem_open/sem_wait ficam na libpthread.
# Nenhuma função pthread_* é usada; a opção serve apenas para ligar os semáforos.
LDLIBS  = -pthread

PROGRAMAS = atendimento analista cliente
# Fontes compartilhados pelo Atendimento e pelos testes
FONTES    = src/nucleo.c src/fila.c src/relogio.c src/recepcao.c
CABECALHOS = src/nucleo.h src/fila.h src/comum.h src/relogio.h src/recepcao.h
TESTES    = $(patsubst tests/%.c,build/tests/%,$(wildcard tests/teste_*.c))

.PHONY: all test clean

all: $(PROGRAMAS)

# Os executáveis ficam na raiz porque o Atendimento cria os clientes com ./cliente
# e todos compartilham o demanda.txt do diretório corrente.
atendimento: src/atendimento.c $(FONTES) $(CABECALHOS)
	$(CC) $(CFLAGS) src/atendimento.c $(FONTES) -o $@ $(LDLIBS)

%: src/%.c src/comum.h
	$(CC) $(CFLAGS) $< -o $@ $(LDLIBS)

build/tests/%: tests/%.c tests/teste.h $(FONTES) $(CABECALHOS)
	@mkdir -p build/tests
	$(CC) $(CFLAGS) -Isrc $< $(FONTES) -o $@ $(LDLIBS)

# Os testes de integração executam os programas compilados
test: $(PROGRAMAS) $(TESTES)
	@echo "Executando testes:"
	@for t in $(TESTES); do ./$$t || exit 1; done

clean:
	rm -rf build $(PROGRAMAS) demanda.txt lng.txt lng.tmp pid_analista.txt
