import { AdaptorErrorMessage } from '@ohos/hvigor-logger';
declare class HvigorTrace {
    static readonly TRACE_KEY = "HVIGOR";
    private data;
    private configBlacklist;
    constructor();
    /**
     * 传输数据给打点管理中心并且在落盘后清理数据
     */
    transmitDataToManager(): void;
    traceTotalTime(totalTime: number): void;
    /**
     * 记录并行、增量、daemon、类型检查的配置的值
     */
    traceBaseConfig(IS_INCREMENTAL: boolean, IS_DAEMON: boolean, IS_PARALLEL: boolean, IS_HVIGORFILE_TYPE_CHECK: boolean): void;
    traceBuildId(buildId: string): void;
    traceTaskTime(taskName: string, moduleName: string, time: number): void;
    traceErrorMessage(adaptorErrorMessage: AdaptorErrorMessage): void;
    insertUsedApi(apiName: string): void;
    traceConfigProperties(configProperties: {
        [key: string]: any;
    }): void;
    traceExperimentFeature(name: string, value: boolean | string): void;
}
export declare const hvigorTrace: HvigorTrace;
export {};
