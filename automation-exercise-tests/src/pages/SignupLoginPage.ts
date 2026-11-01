import { expect, type Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class SignupLoginPage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  async expectSignupSectionVisible() {
    await expect(this.page.getByRole('heading', { name: 'New User Signup!' })).toBeVisible();
  }
  async expectLoginSectionVisible() {
    await expect(this.page.getByRole('heading', { name: 'Login to your account' })).toBeVisible();
  }

  async startSignup(name: string, email: string) {
    await this.page.getByTestId('signup-name').fill(name);
    await this.page.getByTestId('signup-email').fill(email);
    await this.page.getByTestId('signup-button').click();
  }

  async login(email: string, password: string) {
    await this.page.getByTestId('login-email').fill(email);
    await this.page.getByTestId('login-password').fill(password);
    await this.page.getByTestId('login-button').click();
  }

  async expectLoginError() {
    await expect(this.page.getByText('Your email or password is incorrect!')).toBeVisible();
  }
  async expectEmailAlreadyExistsError() {
    await expect(this.page.getByText('Email Address already exist!')).toBeVisible();
  }
}
