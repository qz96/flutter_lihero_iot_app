package com.lh.lhble.listener;

import android.view.View;

public  interface CommontClickListener {
	/**
	 * It informs the listener about new data written in the SRAM
	 * Used to inform about the progress during the SpeedTest
	 *
	 */
      void onViewClick(int position, Object target, View view);
}
