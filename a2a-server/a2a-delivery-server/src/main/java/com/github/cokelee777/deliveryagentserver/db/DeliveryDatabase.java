package com.github.cokelee777.deliveryagentserver.db;

import java.util.Map;
import java.util.Optional;

/**
 * In-memory database for delivery information.
 * <p>
 * This class maintains a static collection of delivery records indexed by tracking
 * number.
 * </p>
 */
public class DeliveryDatabase {

	private static final Map<String, DeliveryInfo> DELIVERIES = Map.of("TRACK-1001",
			new DeliveryInfo("TRACK-1001", "배송중", "서울 강남 허브 출발", "2026-02-25 09:30", "무선 키보드"), "TRACK-2002",
			new DeliveryInfo("TRACK-2002", "배송완료", "수령인 본인 수령 완료", "2026-02-24 14:20", "USB-C 충전기"), "TRACK-3003",
			new DeliveryInfo("TRACK-3003", "상품준비중", "판매자가 상품을 준비하고 있습니다", "2026-02-25 08:00", "모니터 거치대"));

	/**
	 * Looks up delivery information by tracking number.
	 * @param trackingNumber the tracking number to search for
	 * @return a formatted delivery information string, or an error message if not found
	 */
	public static String lookup(String trackingNumber) {
		DeliveryInfo info = DELIVERIES.get(trackingNumber);
		if (info == null) {
			return "[조회 실패] 운송장번호 '" + trackingNumber + "'에 해당하는 배송 정보를 찾을 수 없습니다.";
		}
		return String.format("[배송 조회 결과]\n운송장번호: %s\n상품명: %s\n배송상태: %s\n상세내역: %s\n최종 업데이트: %s", info.trackingNumber(),
				info.productName(), info.status(), info.detail(), info.lastUpdated());
	}

	/**
	 * Finds a delivery record by its tracking number.
	 * @param trackingNumber the tracking number to search for
	 * @return an Optional containing the delivery info, or empty if not found
	 */
	public static Optional<DeliveryInfo> findById(String trackingNumber) {
		return Optional.ofNullable(DELIVERIES.get(trackingNumber));
	}

	/**
	 * Represents a delivery information record.
	 *
	 * @param trackingNumber the unique tracking number
	 * @param status the current delivery status (e.g., "배송중", "배송완료")
	 * @param detail detailed status description
	 * @param lastUpdated the timestamp of the last status update
	 * @param productName the name of the shipped product
	 */
	public record DeliveryInfo(String trackingNumber, String status, String detail, String lastUpdated,
			String productName) {
	}

}
