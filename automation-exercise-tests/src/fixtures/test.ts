import { test as base, expect } from '@playwright/test';
import { createAccount, deleteAccount } from '../api/accountApi';
import { buildUser, type User } from '../data/user';
import {
  CartPage,
  CheckoutPage,
  HomePage,
  ProductDetailPage,
  ProductsPage,
  SignupLoginPage,
  SignupPage,
} from '../pages';

interface Fixtures {
  homePage: HomePage;
  signupLoginPage: SignupLoginPage;
  signupPage: SignupPage;
  productsPage: ProductsPage;
  productDetailPage: ProductDetailPage;
  cartPage: CartPage;
  checkoutPage: CheckoutPage;
  /** Usuário novo (ainda não cadastrado). Removido via API ao final, se existir. */
  user: User;
  /** Usuário já cadastrado via API. Removido via API ao final, se existir. */
  registeredUser: User;
}

const AD_PATTERNS = [
  '**/*googlesyndication*/**',
  '**/*doubleclick*/**',
  '**/*googleadservices*/**',
  '**/pagead/**',
  '**/*adservice.google*/**',
];

export const test = base.extend<Fixtures>({
  // Bloqueia anúncios/vignettes que sobrepõem elementos e causam flakiness.
  page: async ({ page }, use) => {
    for (const pattern of AD_PATTERNS) {
      await page.route(pattern, (route) => route.abort());
    }
    await use(page);
  },

  homePage: async ({ page }, use) => use(new HomePage(page)),
  signupLoginPage: async ({ page }, use) => use(new SignupLoginPage(page)),
  signupPage: async ({ page }, use) => use(new SignupPage(page)),
  productsPage: async ({ page }, use) => use(new ProductsPage(page)),
  productDetailPage: async ({ page }, use) => use(new ProductDetailPage(page)),
  cartPage: async ({ page }, use) => use(new CartPage(page)),
  checkoutPage: async ({ page }, use) => use(new CheckoutPage(page)),

  user: async ({ request }, use) => {
    const user = buildUser();
    await use(user);
    await deleteAccount(request, user);
  },

  registeredUser: async ({ request }, use) => {
    const user = buildUser();
    await createAccount(request, user);
    await use(user);
    await deleteAccount(request, user);
  },
});

export { expect };
