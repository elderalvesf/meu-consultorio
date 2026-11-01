import { expect, type Locator, type Page } from '@playwright/test';

export class FooterComponent {
  private readonly footer: Locator;

  constructor(page: Page) {
    this.footer = page.locator('footer#footer');
  }

  async expectSubscriptionVisible() {
    await this.footer.scrollIntoViewIfNeeded();
    await expect(this.footer.getByRole('heading', { name: 'Subscription' })).toBeVisible();
  }

  async subscribe(email: string) {
    await this.footer.locator('#susbscribe_email').fill(email); // (sic) id com typo no site
    await this.footer.locator('#subscribe').click();
  }

  async expectSubscribed() {
    await expect(this.footer.locator('#success-subscribe')).toContainText(
      'You have been successfully subscribed!',
    );
  }
}
