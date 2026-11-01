import { expect, test } from '../src/fixtures/test';

const SEARCH_TERM = 'Dress';

test('TC09 - Search Product', async ({ homePage, productsPage }) => {
  await homePage.open();
  await homePage.header.goToProducts();
  await productsPage.expectAllProductsVisible();

  await productsPage.search(SEARCH_TERM);
  await productsPage.expectSearchedProductsVisible();

  // O site também busca por categoria/marca, então nem todo resultado tem o termo no
  // nome (ex.: "Sleeves Top and Short"). Validamos que há resultados e que ao menos
  // um produto relacionado ao termo é exibido.
  const names = await productsPage.visibleProductNames();
  expect(names.length).toBeGreaterThan(0);
  expect(names.some((n) => n.toLowerCase().includes(SEARCH_TERM.toLowerCase()))).toBe(true);
});
