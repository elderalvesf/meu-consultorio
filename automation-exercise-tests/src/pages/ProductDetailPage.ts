import { expect, type Locator, type Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class ProductDetailPage extends BasePage {
  private readonly info: Locator;

  constructor(page: Page) {
    super(page);
    this.info = page.locator('.product-information');
  }

  async expectDetailsVisible() {
    await expect(this.page).toHaveURL(/\/product_details\/\d+/);
    await expect(this.info.locator('h2')).not.toBeEmpty(); // nome
    await expect(this.info.getByText(/Category:/)).toBeVisible();
    await expect(this.info.getByText(/Rs\.\s*\d+/)).toBeVisible(); // preço
    await expect(this.info.getByText(/Availability:/)).toBeVisible();
    await expect(this.info.getByText(/Condition:/)).toBeVisible();
    await expect(this.info.getByText(/Brand:/)).toBeVisible();
  }
}
