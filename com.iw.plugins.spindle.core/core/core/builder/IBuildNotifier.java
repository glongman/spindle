package core.builder;

import org.apache.hivemind.Resource;

public interface IBuildNotifier
{ 
    void aboutToProcess(Resource resource);

    void processed(Resource resource);    

    void setProcessingProgressPer(float progress);

    void begin();

    void checkCancel();

    /**
     * Method done.
     */
    void done();

    void updateProgress(float percentComplete);

    float getPercentComplete();

    void updateProgressDelta(float percentWorked);

    void subTask(String message);

}