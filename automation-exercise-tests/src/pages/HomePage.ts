import { expect, type Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class HomePage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  async open() {
    await this.page.goto('/');
    await this.expectLoaded();
  }

  async expectLoaded() {
    await expect(this.page).toHaveURL(/automationexercise\.com\/?$/);
    await expect(this.page.getByRole('heading', { name: 'Full-Fledged practice website for Automation Engineers' })).toBeVisible();
  }
}
