import { expect, type Locator, type Page } from '@playwright/test';

export class HeaderComponent {
  private readonly nav: Locator;

  constructor(private readonly page: Page) {
    this.nav = page.locator('header#header');
  }

  async goToHome() {
    await this.nav.getByRole('link', { name: ' Home' }).click();
  }
  async goToProducts() {
    await this.nav.getByRole('link', { name: ' Products' }).click();
  }
  async goToCart() {
    await this.nav.getByRole('link', { name: ' Cart' }).click();
  }
  async goToSignupLogin() {
    await this.nav.getByRole('link', { name: ' Signup / Login' }).click();
  }
  async logout() {
    await this.nav.getByRole('link', { name: ' Logout' }).click();
  }
  async deleteAccount() {
    await this.nav.getByRole('link', { name: ' Delete Account' }).click();
  }

  async expectLoggedInAs(name: string) {
    await expect(this.nav.getByText(`Logged in as ${name}`)).toBeVisible();
  }
  async expectLoggedOut() {
    await expect(this.nav.getByRole('link', { name: ' Signup / Login' })).toBeVisible();
    await expect(this.nav.getByText('Logged in as')).toHaveCount(0);
  }
}
