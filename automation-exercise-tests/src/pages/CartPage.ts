import { expect, type Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class CartPage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  async expectLoaded() {
    await expect(this.page).toHaveURL(/\/view_cart/);
    await expect(this.page.locator('#cart_info_table')).toBeVisible();
  }

  async expectProductCount(count: number) {
    await expect(this.page.locator('#cart_info_table tbody tr')).toHaveCount(count);
  }

  async proceedToCheckout() {
    await this.page.getByText('Proceed To Checkout').click();
  }

  /** Modal exibido a usuários não autenticados ao tentar fazer checkout. */
  async clickRegisterLoginOnModal() {
    await this.page.locator('#checkoutModal').getByRole('link', { name: 'Register / Login' }).click();
  }
}
