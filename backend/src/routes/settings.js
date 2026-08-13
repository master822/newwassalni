const express = require('express');
const router = express.Router();
const db = require('../database');

/**
 * 1. Get Public App Settings & Remote Config
 */
router.get('/', async (req, res) => {
  try {
    const result = await db.query('SELECT setting_key, setting_value FROM app_settings');
    const settings = {};
    result.rows.forEach(row => {
      settings[row.setting_key] = row.setting_value;
    });

    res.json({
      success: true,
      data: {
        appName: settings.app_name || 'وصلني',
        appTagline: settings.app_tagline || 'نسافر معاً، نوصل بأمان',
        appLogoUrl: settings.app_logo_url || 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=300',
        shamCashAccount: settings.sham_cash_account || 'ba64858e96d4ad9c6096948bc2dbc970',
        isMaintenanceMode: settings.maintenance_mode === 'true',
        ridePublishCost: parseInt(settings.ride_publish_cost || '50', 10),
        appCommissionPercent: parseFloat(settings.app_commission_percent || '5.0'),
        cancellationRefundPercent: parseInt(settings.cancellation_refund_percent || '100', 10),
        appDownloadUrl: settings.app_download_url || 'https://wasalni.app/download',
      },
    });
  } catch (err) {
    console.error('Error fetching settings:', err);
    res.status(500).json({ success: false, error: 'Failed to fetch settings' });
  }
});

module.exports = router;
