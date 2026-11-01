import { request, type FullConfig } from '@playwright/test';

/**
 * Verifica se o site está no ar antes de rodar a suíte. Se estiver indisponível,
 * falha rápido com uma mensagem clara em vez de 10 testes x 3 tentativas com timeout.
 */
export default async function globalSetup(config: FullConfig) {
  const baseURL = config.projects[0].use.baseURL!;
  const context = await request.newContext({ baseURL });
  let lastError = '';

  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const response = await context.get('/api/productsList', { timeout: 30_000 });
      const text = await response.text();
      if (response.ok() && text.trim().startsWith('{')) {
        await context.dispose();
        return;
      }
      lastError = `HTTP ${response.status()} - ${text.slice(0, 120).replace(/\s+/g, ' ')}`;
    } catch (error) {
      lastError = (error as Error).message;
    }
    await new Promise((resolve) => setTimeout(resolve, attempt * 5_000));
  }

  await context.dispose();
  throw new Error(`Site ${baseURL} indisponível após 3 tentativas: ${lastError}`);
}
