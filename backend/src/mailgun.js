/**
 * Mailgun Email Integration Service for Wasalni App
 */
const https = require('https');

async function sendEmail({ to, subject, html, text }) {
  const apiKey = process.env.MAILGUN_API_KEY;
  const domain = process.env.MAILGUN_DOMAIN;
  const host = process.env.MAILGUN_HOST || 'api.mailgun.net'; // 'api.eu.mailgun.net' for EU domains
  const from = process.env.MAILGUN_FROM || `Wasalni App <support@${domain || 'wasalni.app'}>`;

  if (!apiKey || !domain) {
    console.warn('⚠️ Mailgun credentials (MAILGUN_API_KEY / MAILGUN_DOMAIN) not configured. Email logged to console:');
    console.log(`📧 [MAILGUN SIMULATION] To: ${to} | Subject: ${subject}`);
    console.log(`📧 [MAILGUN SIMULATION] Body:\n${text || html}`);
    return {
      success: true,
      simulated: true,
      message: 'Email logged (Mailgun credentials not configured in environment)',
    };
  }

  return new Promise((resolve, reject) => {
    const postData = new URLSearchParams({
      from,
      to,
      subject,
      html: html || text,
      text: text || html,
    }).toString();

    const auth = Buffer.from(`api:${apiKey}`).toString('base64');

    const options = {
      hostname: host,
      port: 443,
      path: `/v3/${domain}/messages`,
      method: 'POST',
      headers: {
        'Authorization': `Basic ${auth}`,
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(postData),
      },
    };

    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            const parsed = JSON.parse(body);
            resolve({ success: true, id: parsed.id, message: parsed.message });
          } catch (e) {
            resolve({ success: true, body });
          }
        } else {
          console.error(`❌ Mailgun HTTP Error ${res.statusCode}:`, body);
          reject(new Error(`Mailgun error (${res.statusCode}): ${body}`));
        }
      });
    });

    req.on('error', (err) => {
      console.error('❌ Mailgun Request Error:', err);
      reject(err);
    });

    req.write(postData);
    req.end();
  });
}

/**
 * Send Password Reset OTP Email
 */
async function sendPasswordResetEmail(toEmail, otpCode, userName = 'مستخدم وصلني') {
  const subject = '🔒 رمز التحقق لإعادة تعيين كلمة المرور - تطبيق وصلني';
  const html = `
    <!DOCTYPE html>
    <html dir="rtl" lang="ar">
    <head>
      <meta charset="utf-8">
      <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; color: #1e293b; }
        .container { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 16px; padding: 32px; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
        .header { text-align: center; margin-bottom: 24px; }
        .logo { font-size: 26px; font-weight: bold; color: #007AFF; }
        .otp-box { background: #f0f7ff; border: 2px dashed #007AFF; border-radius: 12px; padding: 18px; text-align: center; margin: 24px 0; }
        .otp-code { font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #007AFF; font-family: monospace; }
        .footer { font-size: 12px; color: #64748b; text-align: center; margin-top: 24px; border-top: 1px solid #e2e8f0; padding-top: 16px; }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <div class="logo">🚗 وصلني | Wasalni</div>
          <h2 style="color: #0f172a; margin-top: 12px;">إعادة تعيين كلمة المرور</h2>
        </div>
        <p>مرحباً <strong>${userName}</strong>،</p>
        <p>لقد استلمنا طلباً لإعادة تعيين كلمة المرور لحسابك في تطبيق وصلني. استخدم رمز التحقق السري التالي لإتمام العملية:</p>
        
        <div class="otp-box">
          <div class="otp-code">${otpCode}</div>
          <div style="font-size: 12px; color: #64748b; margin-top: 6px;">صلاحية هذا الرمز هي 10 دقائق فقط</div>
        </div>

        <p style="font-size: 13px; color: #64748b;">إذا لم تطلب إعادة تعيين كلمة المرور، يرجى تجاهل هذا البريد الإلكتروني وأمان حسابك سيبقى محمياً.</p>
        
        <div class="footer">
          &copy; ${new Date().getFullYear()} تطبيق وصلني (Wasalni App) - جميع الحقوق محفوظة
        </div>
      </div>
    </body>
    </html>
  `;

  const text = `مرحباً ${userName}،\nرمز التحقق الخاص بإعادة تعيين كلمة المرور لتطبيق وصلني هو: ${otpCode}\nصلاحية الرمز 10 دقائق.`;

  return sendEmail({ to: toEmail, subject, html, text });
}

module.exports = {
  sendEmail,
  sendPasswordResetEmail,
};
