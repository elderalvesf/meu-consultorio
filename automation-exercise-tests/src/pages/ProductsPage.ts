import { expect, type Locator, type Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class ProductsPage extends BasePage {
  private readonly productCards: Locator;

  constructor(page: Page) {
    super(page);
    this.productCards = page.locator('.features_items .product-image-wrapper');
  }

  async expectAllProductsVisible() {
    await expect(this.page).toHaveURL(/\/products/);
    await expect(this.page.getByRole('heading', { name: 'All Products' })).toBeVisible();
    expect(await this.productCards.count()).toBeGreaterThan(0);
  }

  async expectSearchedProductsVisible() {
    await expect(this.page.getByRole('heading', { name: 'Searched Products' })).toBeVisible();
  }

  async search(term: string) {
    await this.page.locator('#search_product').fill(term);
    await this.page.locator('#submit_search').click();
  }

  /** Todos os nomes de produtos exibidos na listagem. */
  async visibleProductNames(): Promise<string[]> {
    const names = await this.productCards.locator('.productinfo p').allTextContents();
    return names.map((n) => n.trim());
  }

  async openFirstProductDetails() {
    await this.page.getByRole('link', { name: 'View Product' }).first().click();
  }

  /** Adiciona o produto da posição `index` (0-based) ao carrinho via hover + overlay. */
  async addToCart(index: number) {
    const card = this.productCards.nth(index);
    await card.scrollIntoViewIfNeeded();
    await card.hover();
    await card.locator('.product-overlay .add-to-cart').click();
  }

  async productInfo(index: number) {
    const card = this.productCards.nth(index);
    return {
      name: (await card.locator('.productinfo p').innerText()).trim(),
      price: (await card.locator('.productinfo h2').innerText()).trim(),
    };
  }

  async continueShopping() {
    await this.page.getByRole('button', { name: 'Continue Shopping' }).click();
  }
  async viewCartFromModal() {
    await this.page.locator('#cartModal').getByRole('link', { name: 'View Cart' }).click();
  }
}
