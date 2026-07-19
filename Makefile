.DEFAULT_GOAL := help

.PHONY: help up down clean test eval e2e e2e-fast e2e-llm logs psql docs

help: ## Show available targets
	@grep -E '^[a-zA-Z0-9_-]+:.*## ' $(MAKEFILE_LIST) | awk -F ':.*## ' '{printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

up: ## Build and start the full stack (app + PostgreSQL)
	docker compose up -d --build --wait

down: ## Stop the stack
	docker compose down

clean: ## Stop the stack and wipe the database volume
	docker compose down -v

test: ## Run unit tests in Docker (no local JDK needed)
	docker run --rm -v "$(CURDIR)":/workspace -v gradle-cache:/home/gradle/.gradle \
		-w /workspace gradle:7.6.1-jdk11 gradle test --no-daemon

eval: ## Run the LLM eval suite against real Gemini (needs GEMINI_API_KEY in .env)
	docker run --rm --env-file .env -v "$(CURDIR)":/workspace -v gradle-cache:/home/gradle/.gradle \
		-w /workspace gradle:7.6.1-jdk11 gradle eval --no-daemon

e2e: ## Build + start stack, then run the Hurl e2e suite
	e2e/run.sh

e2e-fast: ## Run e2e against the already-running stack (no rebuild)
	SKIP_STACK=1 e2e/run.sh

e2e-llm: ## Run e2e incl. the real-Gemini bio scenario (needs GEMINI_API_KEY in .env)
	E2E_LLM=1 e2e/run.sh

docs: up ## Start the stack and open Swagger UI
	open "http://localhost:$${APP_PORT:-8080}/swagger-ui/index.html"

logs: ## Tail app logs
	docker compose logs -f app

psql: ## Open a psql shell in the database container
	docker compose exec db sh -c 'psql -U $$POSTGRES_USER -d $$POSTGRES_DB'
