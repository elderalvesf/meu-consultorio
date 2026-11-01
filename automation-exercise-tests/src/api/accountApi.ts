import type { APIRequestContext, APIResponse } from '@playwright/test';
import type { User } from '../data/user';

/**
 * Helpers da API pública do Automation Exercise (https://www.automationexercise.com/api_list).
 * Usados para preparar/limpar dados sem passar pela UI. A API sempre responde HTTP 200
 * e informa o resultado real em `responseCode` no corpo.
 */
interface ApiBody {
  responseCode: number;
  message: string;
}

async function parse(response: APIResponse, operation: string): Promise<ApiBody> {
  const text = await response.text();
  try {
    return JSON.parse(text) as ApiBody;
  } catch {
    // Quando o site está fora do ar ou bloqueia a requisição, ele devolve HTML.
    throw new Error(
      `${operation}: resposta não-JSON (HTTP ${response.status()}). ` +
        `O site pode estar indisponível. Início do corpo: ${text.slice(0, 120).replace(/\s+/g, ' ')}`,
    );
  }
}

export async function createAccount(request: APIRequestContext, user: User) {
  const response = await request.post('/api/createAccount', {
    form: {
      name: user.name,
      email: user.email,
      password: user.password,
      title: user.title,
      birth_date: user.birthDay,
      birth_month: user.birthMonth,
      birth_year: user.birthYear,
      firstname: user.firstName,
      lastname: user.lastName,
      company: user.company,
      address1: user.address,
      address2: user.address2,
      country: user.country,
      zipcode: user.zipcode,
      state: user.state,
      city: user.city,
      mobile_number: user.mobile,
    },
  });
  const body = await parse(response, 'createAccount');
  if (body.responseCode !== 201) {
    throw new Error(`Falha ao criar conta via API: ${body.responseCode} - ${body.message}`);
  }
}

/**
 * Limpeza "best effort": nunca falha o teste. Se a conta já foi removida pela UI
 * (ou nunca foi criada) ou o site não responder, apenas registra um aviso.
 */
export async function deleteAccount(request: APIRequestContext, user: Pick<User, 'email' | 'password'>) {
  try {
    const response = await request.delete('/api/deleteAccount', {
      form: { email: user.email, password: user.password },
    });
    const body = await parse(response, 'deleteAccount');
    return body.responseCode === 200;
  } catch (error) {
    console.warn(`[cleanup] não foi possível remover ${user.email}: ${(error as Error).message}`);
    return false;
  }
}
