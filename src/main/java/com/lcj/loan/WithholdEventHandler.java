package com.lcj.loan;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service("withhold")
public class WithholdEventHandler implements EventHandler{ 
 
	public String getKey(int type, Object[] para) {		
 
		return UUID.randomUUID().toString();	
 
	}
}
 