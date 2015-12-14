package apiservice

import grails.transaction.Transactional

@Transactional
class CometService {

    void run() {
        100.times {
            sleep(50)
            pushProgress([
                    [id: 1, progress: it],
                    [id: 2, progress: Math.round(it * 1.25)],
                    [id: 3, progress: Math.round(it * 1.5)]
            ])
        }
    }

    private void pushProgress(List<Map> data) {
        eventFor([topic: 'progress'], data)
    }
}
