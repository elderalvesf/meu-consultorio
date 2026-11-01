import { test } from '../src/fixtures/test';

test('TC08 - Verify All Products and product detail page', async ({
  homePage,
  productsPage,
  productDetailPage,
}) => {
  await homePage.open();
  await homePage.header.goToProducts();
  await productsPage.expectAllProductsVisible();

  await productsPage.openFirstProductDetails();
  await productDetailPage.expectDetailsVisible();
});
