import { expect, test } from '../src/fixtures/test';

const SEARCH_TERM = 'Dress';

test('TC09 - Search Product', async ({ homePage, productsPage }) => {
  await homePage.open();
  await homePage.header.goToProducts();
  await productsPage.expectAllProductsVisible();

  await productsPage.search(SEARCH_TERM);
  await productsPage.expectSearchedProductsVisible();

  const names = await productsPage.visibleProductNames();
  expect(names.length).toBeGreaterThan(0);
  for (const name of names) {
    expect(name.toLowerCase()).toContain(SEARCH_TERM.toLowerCase());
  }
});
