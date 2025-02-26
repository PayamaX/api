using System.Collections;
using NHibernate.Criterion;
using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Model;

public class PayamaxRepo(NHibernate.ISession session)
{
    public async Task<int> PayamaksCount()
    {
        return await session.CreateCriteria<PayamakExpectedProcessResultEntity>().SetProjection(Projections.RowCount())
            .UniqueResultAsync<int>();
    }

    public Task Persist(IEnumerable<PayamakExpectedProcessResultEntity> entities, CancellationToken cancellationToken)
    {
        return Task.WhenAll(entities.Select(entity => session.SaveOrUpdateAsync(entity, cancellationToken)));
    }

    public Task<IList<ExpectedPayamakProcessResult>> List(CancellationToken cancellationToken)
    {
        return session.CreateCriteria<ExpectedPayamakProcessResult>().ListAsync<ExpectedPayamakProcessResult>(cancellationToken);
    }
}