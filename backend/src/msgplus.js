const MSGPLUS_BASE_URL =
  process.env.MSGPLUS_BASE_URL || 'https://sms.msgplus.tech/api/v1';

const MSGPLUS_API_KEY = process.env.MSGPLUS_API_KEY;
const MSGPLUS_SENDER_ID = Number(process.env.MSGPLUS_SENDER_ID || 81);
const MSGPLUS_TEMPLATE_ID = Number(process.env.MSGPLUS_TEMPLATE_ID || 1);

async function msgPlusRequest(path, options = {}) {
  if (!MSGPLUS_API_KEY) {
    throw new Error('MSGPLUS_API_KEY is not configured');
  }

  const response = await fetch(`${MSGPLUS_BASE_URL}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${MSGPLUS_API_KEY}`,
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  });

  const text = await response.text();

  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = { raw: text };
  }

  if (!response.ok) {
    const error = new Error(
      data?.error ||
      data?.message ||
      `MsgPlus HTTP ${response.status}`
    );
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

async function sendOtpSms(phone, otp) {
  const timestamp = Math.floor(Date.now() / 1000).toString();

  // Template #1:
  // رمز التحقق الخاص بك هو : [[P1]]
  const payload = {
    sender_id: MSGPLUS_SENDER_ID,
    template_id: MSGPLUS_TEMPLATE_ID,
    numbers: [phone],
    vars: {
      P1: otp,
    },
  };

  return msgPlusRequest('/send', {
    method: 'POST',
    headers: {
      'X-Timestamp': timestamp,
    },
    body: JSON.stringify(payload),
  });
}

module.exports = {
  sendOtpSms,
};
