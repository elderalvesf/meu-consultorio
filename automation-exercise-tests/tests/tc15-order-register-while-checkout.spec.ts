import { validCard } from '../src/data/user';
import { test } from '../src/fixtures/test';

test('TC15 - Place Order: Register while Checkout', async ({
  homePage,
  productsPage,
  cartPage,
  signupLoginPage,
  signupPage,
  checkoutPage,
  user,
}) => {
  await homePage.open();

  await homePage.header.goToProducts();
  await productsPage.addToCart(0);
  await productsPage.viewCartFromModal();
  await cartPage.expectLoaded();
  await cartPage.expectProductCount(1);

  await cartPage.proceedToCheckout();
  await cartPage.clickRegisterLoginOnModal();

  await signupLoginPage.startSignup(user.name, user.email);
  await signupPage.fillAndSubmit(user);
  await signupPage.expectAccountCreated();
  await signupPage.continueAfterCreation();
  await homePage.header.expectLoggedInAs(user.name);

  await homePage.header.goToCart();
  await cartPage.expectLoaded();
  await cartPage.proceedToCheckout();

  await checkoutPage.expectAddressAndReview(user);
  await checkoutPage.addCommentAndPlaceOrder('Pedido de teste automatizado');
  await checkoutPage.pay(validCard);
  await checkoutPage.expectOrderPlaced();

  await homePage.header.deleteAccount();
  await signupPage.expectAccountDeleted();
});
