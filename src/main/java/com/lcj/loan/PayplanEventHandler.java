package com.lcj.loan;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service("payplan")
public class PayplanEventHandler implements EventHandler {

	public String getKey(int type, Object[] para) {

		return UUID.randomUUID().toString();
	}
}
