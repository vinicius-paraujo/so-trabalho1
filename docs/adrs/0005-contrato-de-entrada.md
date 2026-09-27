# ADR 0005: Contrato de entrada e validação

- Status: Aceita
- Data: 20/09/2026
- Responsável pela proposta: Marcos

## Contexto

O enunciado define os campos dos processos e da configuração, mas não determina o tratamento de entradas inválidas nem como o arquivo de configuração será informado ao programa.

## Decisão

1. O caminho do arquivo de configuração será o único argumento da aplicação de terminal.
2. Os processos serão lidos de `stdin` até o fim do fluxo.
3. A entrada deve possuir ao menos um processo.
4. Cada linha deve conter exatamente três inteiros decimais, separados por um ou mais caracteres de espaço em branco.
5. Linhas vazias ou compostas somente por espaços são inválidas.
6. O instante de criação deve ser maior ou igual a zero.
7. A duração e a prioridade estática devem ser maiores que zero.
8. Os identificadores serão atribuídos pela ordem original das linhas, independentemente da ordem de chegada.
9. O arquivo de configuração deve possuir exatamente as chaves `quantum` e `aging`, uma vez cada. A ordem das chaves é livre.
10. Espaços ao redor da chave, do separador `:` e do valor serão aceitos.
11. O quantum deve ser maior que zero e o aging deve ser maior ou igual a zero.
12. Linhas vazias, chaves desconhecidas, chaves duplicadas, valores ausentes e valores não inteiros são inválidos.
13. Uma entrada inválida deve produzir mensagem objetiva em `stderr` e encerramento diferente de zero, sem resultado parcial.

### Complemento de 27/09/2026 (T13–T14)

14. Códigos de encerramento: `0` para sucesso, `1` para configuração ou entrada inválida (inclusive arquivo inexistente ou ilegível) e `2` para quantidade incorreta de argumentos.
15. A entrada padrão é lida em UTF-8, e `stdout` e `stderr` são escritos em UTF-8 em qualquer sistema operacional. Na validação manual, o Windows usou a codificação legada quando os fluxos estavam redirecionados, o que corrompia os acentos das mensagens de erro.
16. Um BOM UTF-8 no início da entrada de processos ou do arquivo de configuração é ignorado. O Windows PowerShell 5.1 insere esse marcador ao encaminhar texto para programas externos (`Get-Content processos.txt | ...`), e o Bloco de Notas pode gravá-lo em arquivos. Sem essa regra, a primeira linha de uma entrada válida seria rejeitada. Um BOM em qualquer outra posição continua inválido.

## Consequências

- O contrato pode ser validado antes da execução dos algoritmos.
- A ordem dos processos permanece rastreável à entrada fornecida pelo professor.
- Casos inválidos produzem falha explícita, em vez de comportamento parcial ou silencioso.
- O uso de aging igual a zero permite desativar o envelhecimento sem alterar o formato da configuração.

