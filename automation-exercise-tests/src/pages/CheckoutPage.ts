import { expect, type Page } from '@playwright/test';
import type { User } from '../data/user';
import type { PaymentCard } from '../data/user';
import { BasePage } from './BasePage';

export class CheckoutPage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  async expectAddressAndReview(user: User) {
    await expect(this.page.getByRole('heading', { name: 'Address Details' })).toBeVisible();
    await expect(this.page.getByRole('heading', { name: 'Review Your Order' })).toBeVisible();
    for (const section of ['#address_delivery', '#address_invoice']) {
      const address = this.page.locator(section);
      await expect(address).toContainText(`${user.firstName} ${user.lastName}`);
      await expect(address).toContainText(user.address);
      await expect(address).toContainText(user.city);
      await expect(address).toContainText(user.mobile);
    }
  }

  async addCommentAndPlaceOrder(comment: string) {
    await this.page.locator('textarea[name="message"]').fill(comment);
    await this.page.getByRole('link', { name: 'Place Order' }).click();
  }

  async pay(card: PaymentCard) {
    await this.page.getByTestId('name-on-card').fill(card.nameOnCard);
    await this.page.getByTestId('card-number').fill(card.number);
    await this.page.getByTestId('cvc').fill(card.cvc);
    await this.page.getByTestId('expiry-month').fill(card.expiryMonth);
    await this.page.getByTestId('expiry-year').fill(card.expiryYear);
    await this.page.getByTestId('pay-button').click();
  }

  async expectOrderPlaced() {
    await expect(this.page.getByTestId('order-placed')).toHaveText('Order Placed!');
    await expect(this.page.getByText('Congratulations! Your order has been confirmed!')).toBeVisible();
  }
}
