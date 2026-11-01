import { randomUUID } from 'node:crypto';

export interface User {
  name: string;
  email: string;
  password: string;
  title: 'Mr' | 'Mrs';
  birthDay: string;
  birthMonth: string; // nome do mês, como exibido no select
  birthYear: string;
  firstName: string;
  lastName: string;
  company: string;
  address: string;
  address2: string;
  country: string;
  state: string;
  city: string;
  zipcode: string;
  mobile: string;
}

export interface PaymentCard {
  nameOnCard: string;
  number: string;
  cvc: string;
  expiryMonth: string;
  expiryYear: string;
}

/** Gera um usuário único por execução para evitar colisões entre testes paralelos. */
export function buildUser(overrides: Partial<User> = {}): User {
  const id = randomUUID().slice(0, 8);
  return {
    name: `qa${id}`,
    email: `qa.${id}.${Date.now()}@example.com`,
    password: 'Senha@12345',
    title: 'Mr',
    birthDay: '10',
    birthMonth: 'May',
    birthYear: '1990',
    firstName: 'Elder',
    lastName: 'Alves',
    company: 'QA Corp',
    address: 'Rua das Flores, 100',
    address2: 'Apto 12',
    country: 'Canada',
    state: 'Ontario',
    city: 'Toronto',
    zipcode: 'M5V 2T6',
    mobile: '+14165550123',
    ...overrides,
  };
}

export const validCard: PaymentCard = {
  nameOnCard: 'Elder Alves',
  number: '4111111111111111',
  cvc: '123',
  expiryMonth: '12',
  expiryYear: '2030',
};
