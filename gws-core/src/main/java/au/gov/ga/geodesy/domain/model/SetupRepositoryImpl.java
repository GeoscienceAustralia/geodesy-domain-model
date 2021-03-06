package au.gov.ga.geodesy.domain.model;

import java.time.Instant;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;

/**
 * {@inheritDoc}
 */
public class SetupRepositoryImpl implements SetupRepositoryCustom {

    @Autowired
    private SetupRepository setups;

    @PersistenceContext(unitName = "geodesy")
    private EntityManager entityManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public Setup findCurrentBySiteId(Integer siteId, SetupType type) {
        // TODO: use query-dsl
        String queryString =
            "select s from Setup s where s.type = :type and s.siteId = :id and s.effectivePeriod.to is null and s.invalidated = false"
            + " order by s.effectivePeriod.from asc nulls first, s.effectivePeriod.to asc nulls last";

        TypedQuery<Setup> query = entityManager.createQuery(queryString, Setup.class);
        query.setParameter("id", siteId);
        query.setParameter("type", type);
        return query.getSingleResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Setup> findBySiteIdAndPeriod(
            Integer siteId,
            SetupType type,
            @Nullable Instant periodStart,
            @Nullable Instant periodEnd,
            Pageable pageRequest) {

        QSetup qsetup = QSetup.setup;

        BooleanExpression isValid = qsetup.invalidated.isFalse();

        BooleanBuilder isContained = new BooleanBuilder();
        BooleanBuilder isInterceptedByPeriodStart = new BooleanBuilder();
        BooleanBuilder isInterceptedByPeriodEnd = new BooleanBuilder();

        if (periodStart != null) {
            isContained.and(qsetup.effectivePeriod.from.goe(periodStart));
            isInterceptedByPeriodStart
                .and(qsetup.effectivePeriod.from.loe(periodStart))
                .and(qsetup.effectivePeriod.to.goe(periodStart));
        }
        if (periodEnd != null) {
            isContained.and(qsetup.effectivePeriod.to.loe(periodEnd));
            isInterceptedByPeriodEnd
                .and(qsetup.effectivePeriod.from.loe(periodEnd))
                .and(qsetup.effectivePeriod.to.goe(periodEnd).or(qsetup.effectivePeriod.to.isNull()));
        }
        Predicate isIntercepted = isInterceptedByPeriodStart.or(isInterceptedByPeriodEnd.getValue()).getValue();

        Predicate requiredSetupPredicate = qsetup.siteId.eq(siteId)
            .and(qsetup.type.eq(type))
            .and(isValid)
            .and(isContained.or(isIntercepted));

        PageRequest newPageRequest = new PageRequest(
            pageRequest.getPageNumber(),
            pageRequest.getPageSize(),
            pageRequest.getSort() != null ? pageRequest.getSort()
                : new Sort(
                    new Sort.Order(Sort.Direction.ASC, "effectivePeriod.from", Sort.NullHandling.NULLS_FIRST),
                    new Sort.Order(Sort.Direction.ASC, "effectivePeriod.to", Sort.NullHandling.NULLS_LAST)
                )
        );

        return setups.findAll(requiredSetupPredicate, newPageRequest);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAllInBatch() {
        entityManager.createQuery("delete from EquipmentInUse").executeUpdate();
        entityManager.createQuery("delete from Setup").executeUpdate();
    }
}
