import type { APIRequestContext } from '@playwright/test';
import type { User } from '../data/user';

/**
 * Helpers da API pública do Automation Exercise (https://www.automationexercise.com/api_list).
 * Usados para preparar/limpar dados sem passar pela UI. A API sempre responde HTTP 200
 * e informa o resultado real em `responseCode` no corpo.
 */
async function parse(response: import('@playwright/test').APIResponse) {
  return JSON.parse(await response.text()) as { responseCode: number; message: string };
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
  const body = await parse(response);
  if (body.responseCode !== 201) {
    throw new Error(`Falha ao criar conta via API: ${body.responseCode} - ${body.message}`);
  }
}

/** Idempotente: se a conta já foi removida pelo teste, apenas ignora. */
export async function deleteAccount(request: APIRequestContext, user: Pick<User, 'email' | 'password'>) {
  const response = await request.delete('/api/deleteAccount', {
    form: { email: user.email, password: user.password },
  });
  const body = await parse(response);
  return body.responseCode === 200;
}
