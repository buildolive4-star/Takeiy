const { describe, it, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");

const PROJECT_ID = "takeit-rules-test";
const DB_ID = "ai-studio-android-takeit-078b58a0-a9d9-4953-b395-099ee3d666f2";
const MAIN_ADMIN_EMAIL = "buildolive4@gmail.com";
const CO_ADMIN_EMAIL = "manager@takeit.app";

let testEnv;

function nowTs() {
  return new Date(Date.now() - 1000);
}

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

function dbFor(uid, email = null, emailVerified = true) {
  if (!uid) return testEnv.unauthenticatedContext().firestore(DB_ID);
  const claims = email ? { email, email_verified: emailVerified } : {};
  return testEnv.authenticatedContext(uid, claims).firestore(DB_ID);
}

describe("Take It Food Delivery & Full-Control Admin Rules", () => {
  it("denies all unauthenticated access", async () => {
    const anon = dbFor(null);
    await assertFails(anon.collection("takeit_restaurants").get());
    await assertFails(anon.collection("takeit_config").doc("live").get());
    await assertFails(anon.collection("takeit_co_admins").doc(CO_ADMIN_EMAIL).get());
  });

  it("allows ONLY Main Admin (buildolive4@gmail.com) to create/list Co-Admins", async () => {
    const mainAdmin = dbFor("admin-1", MAIN_ADMIN_EMAIL, true);
    const customer = dbFor("cust-1", "rohit@gmail.com", true);
    const coAdmin = dbFor("staff-1", CO_ADMIN_EMAIL, true);

    const coAdminPayload = {
      email: CO_ADMIN_EMAIL,
      name: "Amit Manager",
      roleTitle: "Orders & Menu Lead",
      passcode: "TAKEIT@2026",
      activeFlag: true,
      canManageOrders: true,
      canManageRestaurants: true,
      canManageCoupons: false,
      canManageUsers: false,
      canManageSettings: false,
      createdBy: MAIN_ADMIN_EMAIL,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    };

    await assertFails(customer.collection("takeit_co_admins").doc(CO_ADMIN_EMAIL).set(coAdminPayload));
    await assertSucceeds(mainAdmin.collection("takeit_co_admins").doc(CO_ADMIN_EMAIL).set(coAdminPayload));
    await assertSucceeds(mainAdmin.collection("takeit_co_admins").get());
    await assertSucceeds(coAdmin.collection("takeit_co_admins").doc(CO_ADMIN_EMAIL).get());
    await assertFails(coAdmin.collection("takeit_co_admins").get());
    await assertFails(customer.collection("takeit_co_admins").doc(CO_ADMIN_EMAIL).get());
  });

  it("enforces granular Co-Admin permissions vs Customer restrictions on restaurants & coupons", async () => {
    const mainAdmin = dbFor("admin-1", MAIN_ADMIN_EMAIL, true);
    const coAdmin = dbFor("staff-1", CO_ADMIN_EMAIL, true);
    const customer = dbFor("cust-1", "rohit@gmail.com", true);

    await assertSucceeds(
      mainAdmin.collection("takeit_co_admins").doc(CO_ADMIN_EMAIL).set({
        email: CO_ADMIN_EMAIL,
        name: "Menu Staff",
        roleTitle: "Catalog Ops",
        passcode: "PASS99",
        activeFlag: true,
        canManageOrders: true,
        canManageRestaurants: true,
        canManageCoupons: false,
        canManageUsers: false,
        canManageSettings: false,
        createdBy: MAIN_ADMIN_EMAIL,
        createdAt: nowTs(),
        updatedAt: nowTs(),
      })
    );

    const restData = {
      name: "Pizza World",
      cuisine: "Italian, Fast Food",
      category: "Pizza",
      rating: 4.5,
      ratingCount: "10K+",
      deliveryTime: "30 min",
      priceForTwo: 199,
      offerTag: "50% OFF",
      offerCode: "FOOD50",
      address: "Sector 62, Greater Noida",
      imageUrl: "",
      pureVegFlag: true,
      freeDeliveryFlag: true,
      verifiedFlag: true,
      openFlag: true,
      featuredFlag: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    };

    // Customer cannot create restaurant, Co-Admin with permission CAN
    await assertFails(customer.collection("takeit_restaurants").doc("rest_1").set(restData));
    await assertSucceeds(coAdmin.collection("takeit_restaurants").doc("rest_1").set(restData));
    await assertSucceeds(customer.collection("takeit_restaurants").get());

    // Co-Admin without canManageCoupons CANNOT create coupon, Main Admin CAN
    const couponData = {
      code: "FOOD50",
      title: "Flat 50% OFF",
      description: "Up to Rs 120 off on orders above Rs 149",
      discountType: "PERCENT",
      discountValue: 50,
      maxDiscount: 120,
      minOrderAmount: 149,
      activeFlag: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    };
    await assertFails(coAdmin.collection("takeit_coupons").doc("cpn_1").set(couponData));
    await assertSucceeds(mainAdmin.collection("takeit_coupons").doc("cpn_1").set(couponData));
    await assertSucceeds(customer.collection("takeit_coupons").get());
  });

  it("enforces order creation by customer and live status updates by Admin/Co-Admin", async () => {
    const mainAdmin = dbFor("admin-1", MAIN_ADMIN_EMAIL, true);
    const custA = dbFor("cust-a", "a@gmail.com", true);
    const custB = dbFor("cust-b", "b@gmail.com", true);

    const orderData = {
      orderNumber: "TK-1001",
      userId: "cust-a",
      customerName: "Rohit Sharma",
      customerEmail: "a@gmail.com",
      customerPhone: "+91 9876543210",
      restaurantId: "rest_1",
      restaurantName: "Pizza World",
      itemsSummary: "1x Margherita Pizza, 1x Garlic Bread",
      itemTotal: 248,
      deliveryFee: 20,
      platformFee: 5,
      gstAmount: 18,
      discountAmount: 50,
      couponCode: "FOOD50",
      totalAmount: 241,
      paymentMethod: "RAZORPAY",
      paymentStatus: "PAID",
      paymentRef: "pay_123",
      deliveryAddress: "Sector 62, Greater Noida",
      status: "PLACED",
      riderName: "Assigning...",
      riderPhone: "",
      riderVehicle: "",
      etaMinutes: 25,
      otp: "4829",
      rating: 0,
      reviewText: "",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    };

    await assertSucceeds(custA.collection("takeit_orders").doc("ord_1").set(orderData));
    await assertFails(custB.collection("takeit_orders").doc("ord_1").get());
    await assertSucceeds(custA.collection("takeit_orders").where("userId", "==", "cust-a").get());

    // Customer cannot self-approve to DELIVERED, Admin CAN
    await assertFails(
      custA.collection("takeit_orders").doc("ord_1").update({
        status: "DELIVERED",
        updatedAt: nowTs(),
      })
    );
    await assertSucceeds(
      mainAdmin.collection("takeit_orders").doc("ord_1").update({
        status: "OUT_FOR_DELIVERY",
        riderName: "Rahul Kumar",
        riderVehicle: "DL01AB1234",
        etaMinutes: 18,
        updatedAt: nowTs(),
      })
    );
    await assertSucceeds(
      mainAdmin.collection("takeit_orders").doc("ord_1").update({
        status: "DELIVERED",
        etaMinutes: 0,
        updatedAt: nowTs(),
      })
    );
    // Customer can rate after DELIVERED
    await assertSucceeds(
      custA.collection("takeit_orders").doc("ord_1").update({
        rating: 5,
        reviewText: "Hot and delicious!",
        updatedAt: nowTs(),
      })
    );
  });

  it("isolates customer profiles and lets Admin manage users", async () => {
    const mainAdmin = dbFor("admin-1", MAIN_ADMIN_EMAIL, true);
    const custA = dbFor("cust-a", "a@gmail.com", true);
    const custB = dbFor("cust-b", "b@gmail.com", true);

    await assertSucceeds(
      custA.collection("takeit_users").doc("cust-a").set({
        userId: "cust-a",
        name: "Rohit Sharma",
        email: "a@gmail.com",
        phone: "+91 9876543210",
        avatarUrl: "",
        activeAddressLabel: "Home",
        activeAddressLine: "Sector 62, Greater Noida",
        walletBalance: 250,
        goldMemberFlag: true,
        suspendedFlag: false,
        favoriteRestaurantIds: [],
        createdAt: nowTs(),
        updatedAt: nowTs(),
      })
    );

    await assertFails(custB.collection("takeit_users").doc("cust-a").get());
    await assertFails(
      custA.collection("takeit_users").doc("cust-a").update({
        suspendedFlag: true,
        updatedAt: nowTs(),
      })
    );
    await assertSucceeds(
      mainAdmin.collection("takeit_users").doc("cust-a").update({
        walletBalance: 500,
        updatedAt: nowTs(),
      })
    );
  });
});
