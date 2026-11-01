# Automation Exercise – Testes E2E

Trabalho de conclusão: automação dos casos **1, 2, 3, 4, 5, 8, 9, 10, 15 e 16** de
<https://www.automationexercise.com/test_cases>, com **Playwright + TypeScript**.

| TC | Cenário | Arquivo |
|----|---------|---------|
| 01 | Register User | `tests/tc01-register-user.spec.ts` |
| 02 | Login com credenciais corretas | `tests/tc02-login-valid.spec.ts` |
| 03 | Login com credenciais incorretas | `tests/tc03-login-invalid.spec.ts` |
| 04 | Logout | `tests/tc04-logout.spec.ts` |
| 05 | Registro com e-mail existente | `tests/tc05-register-existing-email.spec.ts` |
| 08 | Todos os produtos + detalhe | `tests/tc08-products-and-detail.spec.ts` |
| 09 | Buscar produto | `tests/tc09-search-product.spec.ts` |
| 10 | Subscription na home | `tests/tc10-subscription-home.spec.ts` |
| 15 | Pedido: registrar durante o checkout | `tests/tc15-order-register-while-checkout.spec.ts` |
| 16 | Pedido: login antes do checkout | `tests/tc16-order-login-before-checkout.spec.ts` |

## Boas práticas adotadas

- **Page Object Model** (`src/pages`) com componentes reutilizáveis (header/footer); asserções ficam em métodos `expect*` das páginas.
- **Seletores estáveis**: `getByTestId` mapeado para o atributo `data-qa` do site, `getByRole`/`getByText`; CSS/ID só quando não há alternativa.
- **Fixtures** (`src/fixtures/test.ts`): injeção de páginas e de usuários únicos; pré-condições (conta existente) e limpeza via **API do site** (`/api/createAccount`, `/api/deleteAccount`), mantendo os testes independentes e paralelizáveis.
- **Sem sleeps**: apenas auto-wait e asserções web-first.
- **Anúncios bloqueados** via `page.route` para evitar overlays que quebram cliques.
- Retries (2) e `trace/screenshot/video` somente em CI/falha.

## Executando

```bash
npm ci
npx playwright install chromium
npm test              # headless
npm run test:headed   # com navegador
npm run report        # abre o relatório HTML
npm run typecheck
```

`BASE_URL` pode sobrescrever a URL base.

## Relatórios

- HTML: `playwright-report/`
- JUnit: `test-results/junit.xml`
- Traces/screenshots/vídeos de falhas: `test-results/`

No GitHub Actions (`.github/workflows/playwright.yml`) o relatório HTML e os resultados são publicados como artefatos do run.
