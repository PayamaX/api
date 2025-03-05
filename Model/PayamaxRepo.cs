using System.Collections;
using NHibernate.Criterion;
using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Model;

/// <summary>
/// 
/// </summary>
/// <param name="session"></param>
public class PayamaxRepo(NHibernate.ISession session)
{
    /// <summary>
    /// 
    /// </summary>
    /// <returns></returns>
    public async Task<int> PayamaksCount()
    {
        return await session
            .CreateCriteria<PayamakExpectedProcessResultEntity>()
            .SetProjection(Projections.RowCount())
            .UniqueResultAsync<int>()
            .ConfigureAwait(false);
    }

    /// <summary>
    /// 
    /// </summary>
    /// <param name="payamaks"></param>
    /// <param name="cancellationToken"></param>
    /// <returns></returns>
    public async Task<List<long>> Persist(IEnumerable<PayamakExpectedProcessResultEntity> payamaks,
        CancellationToken cancellationToken)
    {
        var entities = payamaks.ToList();
        await Task.WhenAll(entities.Select(entity => session.SaveOrUpdateAsync(entity, cancellationToken))).ConfigureAwait(false);
        return entities.Select(x => x.Id).ToList();
    }

    /// <summary>
    /// 
    /// </summary>
    /// <param name="cancellationToken"></param>
    /// <returns></returns>
    public Task<IList<PayamakExpectedProcessResultEntity>> List(CancellationToken cancellationToken)
    {
        return session.CreateCriteria<PayamakExpectedProcessResultEntity>()
            .ListAsync<PayamakExpectedProcessResultEntity>(cancellationToken);
    }
}