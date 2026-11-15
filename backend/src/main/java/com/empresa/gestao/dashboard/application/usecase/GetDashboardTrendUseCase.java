package com.empresa.gestao.dashboard.application.usecase;

import com.empresa.gestao.movement.application.dto.TrendPoint;
import com.empresa.gestao.movement.application.usecase.GetMovementTrendUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetDashboardTrendUseCase {

    private final GetMovementTrendUseCase getMovementTrend;

    public GetDashboardTrendUseCase(GetMovementTrendUseCase getMovementTrend) {
        this.getMovementTrend = getMovementTrend;
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> execute(UUID storeId, int days) {
        return getMovementTrend.execute(storeId, days);
    }
}
