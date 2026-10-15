# =================================================================
# Makefile de Infraestrutura (Docker & Docker Compose)
# =================================================================

.PHONY: up down

up:
	# Constrói e executa o Dockerfile/Compose para subir o ambiente
	docker compose up -d --build
	@echo "🚀 Infraestrutura iniciada com sucesso!"

down:
	# Encerra os containers e remove imagens/cache órfãs para liberar memória
	docker compose down --rmi local --remove-orphans
	@echo "🛑 Infraestrutura encerrada e limpa da memória."