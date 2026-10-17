CC      = gcc
CFLAGS  = -Wall -Wextra -std=gnu11 -g
# Em versões da glibc anteriores à 2.34, sem_open/sem_wait ficam na libpthread.
# Nenhuma função pthread_* é usada; a opção serve apenas para ligar os semáforos.
LDLIBS  = -pthread

PROGRAMAS = atendimento analista cliente
KERNEL    = src/kernel.c src/kernel.h
TESTES    = $(patsubst tests/%.c,build/tests/%,$(wildcard tests/test_*.c))

.PHONY: all test clean

all: $(PROGRAMAS)

# Os executáveis ficam na raiz porque o Atendimento cria os clientes com ./cliente
# e todos compartilham o demanda.txt do diretório corrente.
atendimento: src/atendimento.c $(KERNEL)
	$(CC) $(CFLAGS) src/atendimento.c src/kernel.c -o $@ $(LDLIBS)

%: src/%.c
	$(CC) $(CFLAGS) $< -o $@ $(LDLIBS)

build/tests/%: tests/%.c tests/teste.h $(KERNEL)
	@mkdir -p build/tests
	$(CC) $(CFLAGS) -Isrc $< src/kernel.c -o $@ $(LDLIBS)

test: $(TESTES)
	@echo "Executando testes:"
	@for t in $(TESTES); do ./$$t || exit 1; done

clean:
	rm -rf build $(PROGRAMAS) demanda.txt
