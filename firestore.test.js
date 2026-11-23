const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const ADMIN_UID = "admin_uid_999";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(msAgo = 2000) {
  return new Date(Date.now() - msAgo);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write any user profile or subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      userName: "Ghost",
      walletBalance: 0,
      rewardPoints: 0,
      activePanelName: "CUSTOMER",
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
});

test("Owner can create and update their own UserProfile; shadow fields & cross-user reads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastTime(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      userName: "Rohit Sharma",
      userPhone: "+91 98765 43210",
      userEmail: "rohit@takeit.app",
      activeAddressLabel: "Sector 62, Noida",
      activeAddressLine: "Sector 62, Noida • Electronic City",
      walletBalance: 2450.0,
      rewardPoints: 1280,
      activePanelName: "CUSTOMER",
      driverOnline: true,
      deliveryPartnerOnline: true,
      language: "English (India)",
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletBalance: 2950.0,
      updatedAt: pastTime(1000),
    })
  );

  // Shadow update (ghost field `isAdmin`) must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastTime(500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userName: "Future User",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user read and write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      walletBalance: 0,
      updatedAt: pastTime(500),
    })
  );
});

test("Order lifecycle: create, status update, terminal lock on COMPLETED, and post-complete rating", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastTime(8000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ORD_1001").set({
      userId: ALICE_UID,
      orderType: "FOOD",
      title: "Pizza World • 2 items",
      subtitle: "Margherita + Garlic Bread",
      status: "PREPARING",
      amount: 241.0,
      paymentMethod: "Wallet",
      pickupLocation: "Pizza World, Noida",
      dropLocation: "Sector 62, Noida",
      otp: "4821",
      partnerName: "Rahul Kumar",
      rating: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List own orders succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("orders").get());

  // Cross-user list fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").get());

  // Transition to COMPLETED succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ORD_1001").update({
      status: "COMPLETED",
      updatedAt: pastTime(3000),
    })
  );

  // Rating after COMPLETED succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ORD_1001").update({
      rating: 5,
      updatedAt: pastTime(2000),
    })
  );

  // Re-opening a COMPLETED order back to PENDING must fail (terminal state lock)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ORD_1001").update({
      status: "PENDING",
      updatedAt: pastTime(1000),
    })
  );

  // Including `@DocumentId` `id` field in create payload must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ORD_1002").set({
      id: "ORD_1002",
      userId: ALICE_UID,
      orderType: "RIDE",
      title: "Bike Taxi",
      subtitle: "Fast",
      status: "ON_THE_WAY",
      amount: 49.0,
      paymentMethod: "Cash",
      pickupLocation: "A",
      dropLocation: "B",
      otp: "1234",
      partnerName: "Rohit",
      rating: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Wallet transactions are append-only; Menu, Grocery, Notifications, and Addresses enforce owner bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastTime(5000);

  // Wallet tx create succeeds, update/delete fail
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wallet_transactions").doc("TX_1").set({
      userId: ALICE_UID,
      title: "Wallet Top-up",
      txType: "CREDIT",
      amount: 500.0,
      method: "UPI",
      createdAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("wallet_transactions").doc("TX_1").update({
      amount: 99999.0,
    })
  );

  // Menu item create & toggle availability
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("m_1").set({
      userId: ALICE_UID,
      restaurantId: "r1",
      category: "Pizza",
      name: "Margherita Pizza",
      description: "Classic mozzarella & basil",
      price: 149,
      vegFlag: true,
      availableFlag: true,
      bestsellerFlag: true,
      rating: 4.6,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("m_1").update({
      availableFlag: false,
      updatedAt: pastTime(1000),
    })
  );

  // Grocery stock create & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("grocery_products").doc("g_1").set({
      userId: ALICE_UID,
      name: "Fresh Apple",
      category: "Fruits",
      unit: "1 kg",
      price: 69,
      mrp: 90,
      stockCount: 40,
      badge: "Farm Fresh",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("grocery_products").doc("g_1").update({
      stockCount: 38,
      updatedAt: pastTime(1000),
    })
  );

  // Notification readFlag update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n_1").set({
      userId: ALICE_UID,
      title: "Order Confirmed",
      body: "Your order is on the way",
      category: "ORDER",
      readFlag: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n_1").update({
      readFlag: true,
      updatedAt: pastTime(1000),
    })
  );

  // Saved GPS address create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_home").set({
      userId: ALICE_UID,
      label: "Home GPS",
      fullAddress: "Sector 62, Noida, UP",
      landmark: "Near Metro Gate 2",
      latitude: 28.6279,
      longitude: 77.3649,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Email spoofing test: unverified admin email is rejected; verified admin email succeeds", async () => {
  const created = pastTime(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      userName: "Alice",
      walletBalance: 100,
      rewardPoints: 10,
      activePanelName: "CUSTOMER",
      createdAt: created,
      updatedAt: created,
    });
  });

  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: "buildolive4@gmail.com", email_verified: false })
    .firestore();
  await assertFails(spoofedAdminDb.collection("users").doc(ALICE_UID).get());

  const realAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: "buildolive4@gmail.com", email_verified: true })
    .firestore();
  await assertSucceeds(realAdminDb.collection("users").doc(ALICE_UID).get());
});
