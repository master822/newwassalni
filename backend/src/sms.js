/**
 * MSGPlus SMS Gateway Integration Service for Wasalni App
 */
const https = require('https');
const http = require('http');

/**
 * Format Syrian / International phone number
 * converts 0988123456 -> 963988123456
 */
function formatPhoneNumber(phone) {
  if (!phone) return '';
  let clean = phone.toString().replace(/[\s\-\(\)\+]/g, '');
  if (clean.startsWith('00')) {
    clean = clean.substring(2);
  }

  if (clean.startsWith('09') && clean.length === 10) {
    clean = '963' + clean.substring(1);
  }

  return clean;
}

/**
 * Send SMS via MSGPlus API
 * @param {string} phone - Recipient phone number
 * @param {string} message - Message body or OTP
 * @param {object} options - Optional overrides
 */
async function sendSms(phone, message, options = {}) {
  const apiKey = options.apiKey || process.env.MSGPLUS_API_KEY;
  const baseUrl = options.baseUrl || process.env.MSGPLUS_BASE_URL || 'https://sms.msgplus.tech/api/v1';
  const senderId = options.senderId || process.env.MSGPLUS_SENDER_ID || '81';
  const templateId = options.templateId || process.env.MSGPLUS_TEMPLATE_ID || '1';

  const formattedPhone = formatPhoneNumber(phone);

  if (!apiKey) {
    console.warn('⚠️ MSGPlus API key not set. SMS logged to console:');
    console.log(`📱 [SMS SIMULATION] To: ${formattedPhone} (${phone}) | Message: "${message}"`);
    return {
      success: true,
      simulated: true,
      phone: formattedPhone,
      message: 'SMS logged (MSGPLUS_API_KEY not configured)',
    };
  }

  return new Promise((resolve) => {
    try {
      const url = new URL(`${baseUrl.replace(/\/+$/, '')}/sms/send`);
      const payload = JSON.stringify({
        api_key: apiKey,
        sender_id: senderId,
        template_id: templateId,
        phone: formattedPhone,
        mobile: formattedPhone,
        to: formattedPhone,
        message: message,
        text: message,
      });

      const isHttps = url.protocol === 'https:';
      const client = isHttps ? https : http;

      const reqOptions = {
        hostname: url.hostname,
        port: url.port || (isHttps ? 443 : 80),
        path: url.pathname + url.search,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${apiKey}`,
          'apikey': apiKey,
          'Accept': 'application/json',
          'Content-Length': Buffer.byteLength(payload),
        },
        timeout: 10000,
      };

      const req = client.request(reqOptions, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          console.log(`📱 [MSGPlus SMS] Status: ${res.statusCode} | To: ${formattedPhone} | Response: ${data}`);
          if (res.statusCode >= 200 && res.statusCode < 300) {
            try {
              const json = JSON.parse(data);
              resolve({ success: true, response: json });
            } catch (e) {
              resolve({ success: true, raw: data });
            }
          } else {
            console.error(`❌ [MSGPlus SMS Error] Status ${res.statusCode}: ${data}`);
            resolve({ success: false, statusCode: res.statusCode, error: data });
          }
        });
      });

      req.on('error', (err) => {
        console.error('❌ [MSGPlus Network Error]:', err.message);
        resolve({ success: false, error: err.message });
      });

      req.on('timeout', () => {
        req.destroy();
        console.error('❌ [MSGPlus Timeout Error]: Request timed out after 10s');
        resolve({ success: false, error: 'Timeout connecting to MSGPlus' });
      });

      req.write(payload);
      req.end();
    } catch (err) {
      console.error('❌ [MSGPlus Exception]:', err.message);
      resolve({ success: false, error: err.message });
    }
  });
}

/**
 * Send OTP SMS
 */
async function sendOtpSms(phone, otpCode) {
  const message = `رمز التحقق الخاص بك لتطبيق وصلني هو: ${otpCode} (صالح لمدة 5 دقائق). لا تشارك الرمز مع أي شخص.`;
  return sendSms(phone, message);
}

module.exports = {
  sendSms,
  sendOtpSms,
  formatPhoneNumber,
};
