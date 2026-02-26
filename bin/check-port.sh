#!/bin/bash

# check-port.sh: Scannerizza una porta, mostra PID e conferma kill se richiesto

if [ $# -ne 2 ]; then
  echo "Uso: $0 [port] [go/no]"
  echo "Esempio: $0 8080 go"
  exit 1
fi

PORT=$1
ACTION=$2

# Verifica porta con lsof
PIDS=$(sudo lsof -t -i:$PORT)

if [ -z "$PIDS" ]; then
  echo "Nessun processo trovato sulla porta $PORT."
  exit 0
fi

echo "Processi sulla porta $PORT (PID): $PIDS"
echo "Dettagli:"
sudo lsof -i :$PORT

if [ "$ACTION" = "no" ]; then
  echo "..."
  exit 0
fi


if [ "$ACTION" = "go" ]; then
  echo "Kill in corso..."
  for PID in $PIDS; do
    sudo kill -9 $PID
  done
  echo "Processi terminati sulla porta $PORT."
else
  echo "Azione '$ACTION' non riconosciuta. Usa 'go' per confermare il kill."
fi

