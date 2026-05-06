package com.example.tracking_order.modules.order.service.impl;

import com.example.tracking_order.modules.order.entity.Order;
import com.example.tracking_order.modules.order.enums.OrderStatus;
import com.example.tracking_order.modules.order.repository.OrderRepository;
import com.example.tracking_order.modules.order.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrdersToCsv(OrderStatus status, LocalDateTime fromDate, LocalDateTime toDate) {
        Specification<Order> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        // Giới hạn 1000 record để tránh OOM, trong thực tế nên dùng Pageable từ Controller
        Pageable limit = PageRequest.of(0, 1000);
        List<Order> orders = orderRepository.findAll(spec, limit).getContent();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);

        try (PrintWriter pw = new PrintWriter(out, false, StandardCharsets.UTF_8)) {
            pw.println("Order Code,User,Grand Total,Status,Payment Status,Payment Method,Created At");

            for (Order order : orders) {
                pw.printf("%s,%s,%s,%s,%s,%s,%s\n",
                        escapeCsv(order.getOrderCode()),
                        escapeCsv(order.getUser().getFullName()),
                        order.getGrandTotal(),
                        order.getStatus().name(),
                        order.getPaymentStatus().name(),
                        order.getPaymentMethod().name(),
                        order.getCreatedAt().toString()
                );
            }
            pw.flush();
        }

        return out.toByteArray();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
