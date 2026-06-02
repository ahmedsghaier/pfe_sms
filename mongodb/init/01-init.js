// DB AUTH
db = db.getSiblingDB('easybulk_auth');

// Collections
db.createCollection('users');
db.createCollection('organizations');
db.createCollection('user_organization_roles');

// Index
db.users.createIndex({ email: 1 }, { unique: true });
db.organizations.createIndex({ name: 1 });
db.user_organization_roles.createIndex({ userId: 1, organizationId: 1 });

// Insert admin
const adminId = new ObjectId();

db.users.insertOne({
    _id: adminId,
    email: 'admin@easybulk.com',
    password: '$2a$10$XqzY5MzGVLh7GqWLqT8TJ.OdCB1F0Y5.X8wZBXqH5vxrP1GqUPJqm', //admin123
    firstName: 'Super',
    lastName: 'Admin',
    active: true,
    emailVerified: true,
    createdAt: new Date(),
    updatedAt: new Date(),
    organizationIds: [],
    isAdUser: false
});

// Organization
const orgId = new ObjectId();

db.organizations.insertOne({
    _id: orgId,
    name: 'Organisation par défaut',
    description: 'Organisation initiale EasyBulk',
    active: true,
    createdAt: new Date(),
    updatedAt: new Date()
});

// Role
db.user_organization_roles.insertOne({
    userId: adminId.toString(),
    organizationId: orgId.toString(),
    groupId: null,
    role: 'SUPERADMIN'
});