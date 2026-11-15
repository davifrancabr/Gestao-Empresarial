package com.empresa.gestao.movement.application.usecase;

import com.empresa.gestao.movement.domain.repository.DailyMovementCount;
import com.empresa.gestao.movement.application.dto.TrendPoint;
import com.empresa.gestao.movement.domain.repository.StockMovementRepository;
import com.empresa.gestao.shared.domain.model.MovementType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GetMovementTrendUseCase {

    private final StockMovementRepository movementRepository;

    public GetMovementTrendUseCase(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> execute(UUID storeId, int days) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = today.minusDays(safeDays - 1L);
        Instant since = start.atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<LocalDate, long[]> byDay = new LinkedHashMap<>();
        for (int i = 0; i < safeDays; i++) {
            byDay.put(start.plusDays(i), new long[]{0, 0});
        }

        for (DailyMovementCount daily : movementRepository.countDaily(storeId, since)) {
            long[] values = byDay.get(daily.day());
            if (values == null) {
                continue;
            }
            if (daily.type() == MovementType.ENTRY) {
                values[0] = daily.total();
            } else if (daily.type() == MovementType.EXIT) {
                values[1] = daily.total();
            }
        }

        List<TrendPoint> trend = new ArrayList<>();
        byDay.forEach((date, values) -> trend.add(new TrendPoint(date, values[0], values[1])));
        return trend;
    }
}
