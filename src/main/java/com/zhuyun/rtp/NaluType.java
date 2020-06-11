package com.zhuyun.rtp;

/**
 * H264��NALU type
 * @author zhouyinfei
 *
 */
public enum NaluType {
	FU_A((byte) 28),				//��Ƭ���ģʽ
	FU_B((byte) 29),
	STAP_A((byte) 24),				//��Ϸ��ģʽ
	STAP_B((byte) 25),			
	MTAP_16((byte) 26),			
	MTAP_24((byte) 27),		
	SINGLE((byte) 1);						//��һNAL ��Ԫģʽ
	
	private byte type;
	
	private NaluType(byte type) {
		this.type = type;
	}

	public byte getType() {
		return type;
	}
}
