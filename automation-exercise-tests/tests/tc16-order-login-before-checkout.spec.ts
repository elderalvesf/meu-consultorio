import { validCard } from '../src/data/user';
import { test } from '../src/fixtures/test';

test('TC16 - Place Order: Login before Checkout', async ({
  homePage,
  productsPage,
  cartPage,
  signupLoginPage,
  signupPage,
  checkoutPage,
  registeredUser,
}) => {
  await homePage.open();
  await homePage.header.goToSignupLogin();
  await signupLoginPage.login(registeredUser.email, registeredUser.password);
  await homePage.header.expectLoggedInAs(registeredUser.name);

  await homePage.header.goToProducts();
  await productsPage.addToCart(0);
  await productsPage.viewCartFromModal();
  await cartPage.expectLoaded();
  await cartPage.expectProductCount(1);

  await cartPage.proceedToCheckout();
  await checkoutPage.expectAddressAndReview(registeredUser);
  await checkoutPage.addCommentAndPlaceOrder('Pedido de teste automatizado');
  await checkoutPage.pay(validCard);
  await checkoutPage.expectOrderPlaced();

  await homePage.header.deleteAccount();
  await signupPage.expectAccountDeleted();
});
