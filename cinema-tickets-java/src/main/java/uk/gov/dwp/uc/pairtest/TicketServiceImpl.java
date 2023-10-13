package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import java.util.Arrays;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_TICKETS_PER_PURCHASE = 20;
    private static final int CHILD_TICKET_PRICE = 10;
    private static final int ADULT_TICKET_PRICE = 20;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }
    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        // Validate the ticket purchase requests
        isValidTicketPurchase(accountId, ticketTypeRequests);

        // Calculate the total price and reserve seats
        int totalPrice = calculateTotalPrice(ticketTypeRequests);
        ticketPaymentService.makePayment(accountId,totalPrice);

        // Make a seat reservation request
        int totalSeatsToReserve = calculateTotalSeatsToReserve(ticketTypeRequests);
        seatReservationService.reserveSeat(accountId, totalSeatsToReserve);

        // Print a success message
        System.out.println("Tickets purchased successfully!");
    }

    private static void isValidTicketPurchase(Long accountId, TicketTypeRequest... ticketTypeRequests) {
        // Validate accountId
        if(accountId<=0){
            System.out.println("Invalid accountId");
            throw new InvalidPurchaseException();
        }

        // Validate total number of purchased tickets
        int totalQuantity = Arrays.stream(ticketTypeRequests).mapToInt(TicketTypeRequest::getNoOfTickets).sum();
        if (totalQuantity > MAX_TICKETS_PER_PURCHASE) {
            System.out.println("Exceeded maximum allowed tickets per purchase");
            throw new InvalidPurchaseException();
        }

        // Validate the combination of ticket types
        boolean hasAdultTicket = Arrays.stream(ticketTypeRequests)
                .anyMatch(request -> request.getTicketType() == TicketTypeRequest.Type.ADULT);
        boolean hasChildOrInfantTicket = Arrays.stream(ticketTypeRequests)
                .anyMatch(request -> request.getTicketType() == TicketTypeRequest.Type.CHILD || request.getTicketType() == TicketTypeRequest.Type.INFANT);

        if (hasChildOrInfantTicket && !hasAdultTicket) {
            System.out.println("Child or Infant tickets cannot be purchased without an Adult ticket");
            throw new InvalidPurchaseException();
        }
    }

    private int calculateTotalPrice(TicketTypeRequest... ticketRequests) {
        return Arrays.stream(ticketRequests)
                .mapToInt(request -> switch (request.getTicketType()) {
                    case CHILD -> CHILD_TICKET_PRICE * request.getNoOfTickets();
                    case ADULT -> ADULT_TICKET_PRICE * request.getNoOfTickets();
                    case INFANT -> 0; // INFANT tickets are free
                    default -> throw new InvalidPurchaseException();
                })
                .sum();
    }

    private int calculateTotalSeatsToReserve(TicketTypeRequest... ticketRequests) {
        return Arrays.stream(ticketRequests)
                //Infants are not allocated a seat.
                .filter(ticketTypeRequest -> !ticketTypeRequest.getTicketType().equals(TicketTypeRequest.Type.INFANT))
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();
    }

}
