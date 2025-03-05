using FluentNHibernate.Conventions;
using FluentNHibernate.Conventions.Instances;

namespace PayamaX.Portal.Config.NHibernateFluentAutoMap;

/// <summary>
/// 
/// </summary>
public class PayamaxConventions : IClassConvention
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="instance"></param>
    public void Apply(IClassInstance instance)
    {
        ArgumentNullException.ThrowIfNull(instance);
        instance.Table($"\"{instance.EntityType.Name[..^"Entity".Length]}\"");
    }
}