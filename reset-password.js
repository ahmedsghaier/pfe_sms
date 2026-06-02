const bcrypt = require('bcryptjs');

const password = 'admin123';
const saltRounds = 10;

bcrypt.hash(password, saltRounds, function(err, hash) {
    if (err) {
        console.error('Error:', err);
    } else {
        console.log('Hash for admin123:');
        console.log(hash);
        console.log('\nMongoDB update command:');
        console.log(`db.users.updateOne({email:"admin@easybulk.com"},{$set:{password:"${hash}"}})`);
    }
});