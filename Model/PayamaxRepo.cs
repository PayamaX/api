using NHibernate.Criterion;

namespace PayamaX.Portal.Model;

public class PayamaxRepo
{
    private readonly NHibernate.ISession session;

    public PayamaxRepo(NHibernate.ISession session)
    {
        this.session = session;
    }

    public async Task<int> PayamaksCount()
    {
        return await session.CreateCriteria<PayamakExpectedProcessResultEntity>().SetProjection(Projections.RowCount())
            .UniqueResultAsync<int>();
    }

    public Task Persist(IEnumerable<PayamakExpectedProcessResultEntity> entities, CancellationToken cancellationToken)
    {
        return Task.WhenAll(entities.Select(entity => session.SaveOrUpdateAsync(entity, cancellationToken)));
    }
}